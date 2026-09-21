const WS_TIMEOUT_MS = (() => {
  const value = Number(import.meta.env.VITE_INTERVIEW_WS_TIMEOUT_MS || 120000);
  return Number.isFinite(value) && value > 0 ? Math.floor(value) : 120000;
})();

const resolveInterviewWsUrl = () => {
  const configured = String(import.meta.env.VITE_INTERVIEW_WS_URL || '').trim();
  if (configured) {
    return configured;
  }
  if (typeof window === 'undefined') {
    return '';
  }
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
  const host = window.location.hostname || '127.0.0.1';
  const port = String(import.meta.env.VITE_INTERVIEW_WS_PORT || '8010').trim();
  return `${protocol}://${host}:${port}/api/interviews/ws`;
};

const WS_URL = resolveInterviewWsUrl();

let socket = null;
let connectPromise = null;
const pendingRequests = new Map();

const clearPendingRequests = (reason) => {
  pendingRequests.forEach(({ reject, timer }) => {
    if (timer) {
      clearTimeout(timer);
    }
    reject(new Error(reason));
  });
  pendingRequests.clear();
};

const handleSocketMessage = (raw) => {
  let message;
  try {
    message = JSON.parse(raw.data);
  } catch (error) {
    return;
  }

  const requestId = String(message.request_id || '');
  if (!requestId) {
    return;
  }

  const request = pendingRequests.get(requestId);
  if (!request) {
    return;
  }

  if (message.type === 'event') {
    request.events.push(message.event || {});
    return;
  }

  if (message.type !== 'response') {
    return;
  }

  pendingRequests.delete(requestId);
  if (request.timer) {
    clearTimeout(request.timer);
  }

  if (message.ok) {
    request.resolve({
      data: message.data || {},
      eventLog: request.events,
    });
    return;
  }

  const error = new Error(message.error || 'Interview websocket request failed');
  error.status = message.status_code || 500;
  error.payload = message;
  request.reject(error);
};

const ensureSocket = () => {
  if (!WS_URL || typeof WebSocket === 'undefined') {
    return Promise.reject(new Error('Interview websocket is not available'));
  }

  if (socket && socket.readyState === WebSocket.OPEN) {
    return Promise.resolve(socket);
  }

  if (connectPromise) {
    return connectPromise;
  }

  connectPromise = new Promise((resolve, reject) => {
    const ws = new WebSocket(WS_URL);

    ws.onopen = () => {
      socket = ws;
      resolve(ws);
    };

    ws.onmessage = handleSocketMessage;

    ws.onclose = () => {
      socket = null;
      connectPromise = null;
      clearPendingRequests('Interview websocket disconnected');
    };

    ws.onerror = () => {
      if (ws.readyState !== WebSocket.OPEN) {
        connectPromise = null;
        reject(new Error('Interview websocket connection failed'));
      }
    };
  });

  return connectPromise;
};

const buildRequestId = () =>
  `req_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;

export const isInterviewWsEnabled = () => Boolean(WS_URL && typeof WebSocket !== 'undefined');

export const sendInterviewWsRequest = async (action, payload = {}) => {
  const ws = await ensureSocket();
  const requestId = buildRequestId();

  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      pendingRequests.delete(requestId);
      reject(new Error(`Interview websocket timeout after ${WS_TIMEOUT_MS}ms`));
    }, WS_TIMEOUT_MS);

    pendingRequests.set(requestId, {
      resolve,
      reject,
      events: [],
      timer,
    });

    try {
      ws.send(
        JSON.stringify({
          request_id: requestId,
          action,
          payload,
        }),
      );
    } catch (error) {
      pendingRequests.delete(requestId);
      clearTimeout(timer);
      reject(error);
    }
  });
};

export const closeInterviewWs = () => {
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.close(1000, 'client reset');
  }
  socket = null;
  connectPromise = null;
  clearPendingRequests('Interview websocket closed');
};
