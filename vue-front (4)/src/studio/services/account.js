export const ACCOUNT_KEY = 'interview-studio:profile:v1';
const defaults = () => ({ nickname: '小林', roleId: 'java', level: '校招', graduation: '2027', avatar: '', signedIn: false });
export function createAccountService(storage) {
  let profile = defaults(), warning = '';
  try { const raw = JSON.parse(storage?.getItem(ACCOUNT_KEY) || 'null'); if (raw && typeof raw.nickname === 'string') profile = { ...profile, ...raw }; } catch { warning = '资料读取失败，已使用默认资料。'; }
  const save = next => {
    try { if (!storage) throw Error(); storage.setItem(ACCOUNT_KEY, JSON.stringify(next)); }
    catch { throw Error('本地保存失败，请检查浏览器存储空间。原资料未改变。'); }
    profile = next; warning = '';
  };
  return {
    snapshot: () => ({ ...profile, warning }),
    update(input) {
      const nickname = String(input.nickname ?? profile.nickname).trim();
      if (!nickname || nickname.length > 20) throw Error('昵称需要 1–20 个字符。');
      const roleId = input.roleId ?? profile.roleId, level = input.level ?? profile.level;
      if (!['java','web','algorithm','testing'].includes(roleId) || !['校招','初级','中级'].includes(level)) throw Error('请选择有效岗位与阶段。');
      const avatar = input.avatar ?? profile.avatar;
      if (avatar && (!/^data:image\/(png|jpeg|webp);base64,/.test(avatar) || avatar.length > 700000)) throw Error('头像无效或过大，请重新裁剪。');
      save({ ...profile, nickname, roleId, level, graduation: String(input.graduation ?? profile.graduation).slice(0,4), avatar });
    },
    login({method='code', credential, agreed}) {
      if (!agreed) throw Error('请先确认演示说明。');
      if (credential !== (method === 'code' ? '123456' : 'demo123456')) throw Error(method === 'code' ? '演示验证码为 123456。' : '演示密码为 demo123456。');
      save({ ...profile, signedIn: true });
    },
    register({nickname, code, agreed}) {
      if (!agreed || code !== '123456') throw Error('请确认演示说明，并输入演示验证码 123456。');
      const name = String(nickname || '').trim(); if (!name || name.length > 20) throw Error('昵称需要 1–20 个字符。');
      save({ ...profile, nickname: name, signedIn: true });
    },
    logout() { save({ ...profile, signedIn: false }); },
  };
}
