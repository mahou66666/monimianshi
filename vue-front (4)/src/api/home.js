import request from '@/utils/request';

export const getHomeContent = () => request.get('/api/home/content');
