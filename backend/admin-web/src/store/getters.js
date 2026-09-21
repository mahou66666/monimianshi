const getters = {
  sidebar: state => state.app.sidebar,
  size: state => state.app.size,
  device: state => state.app.device,
  visitedViews: state => state.tagsView.visitedViews,
  cachedViews: state => state.tagsView.cachedViews,
  token: state => state.user.token,
  avatar: state => state.user.avatar,
  name: state => state.user.name,
  introduction: state => state.user.introduction,
  roles: state => state.user.roles,
  permission_codes: state => state.user.permissionCodes,
  permission_routes: state => state.permission.routes,
  sidebar_routes: state => state.permission.sidebarRoutes,
  legacy_routes: state => state.permission.legacyRoutes,
  errorLogs: state => state.errorLog.logs
}
export default getters
