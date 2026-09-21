import request from '@/utils/request';

export const parseJd = (data) => request.post('/api/jd/parse', data);
