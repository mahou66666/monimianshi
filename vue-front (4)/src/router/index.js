import { createRouter, createWebHistory } from 'vue-router';
import { routes } from './realRoutes.js';
const router=createRouter({history:createWebHistory(),routes});
router.afterEach(to=>{document.title=`${to.meta.title||'面试精灵'} | 面试精灵`;});
export default router;
