export const getAdminInfo = () => JSON.parse(localStorage.getItem('admin_info') || '{}')

export const getPermissionCodes = () => {
  const info = getAdminInfo()
  return info.permissionCodes || info.permissions || []
}

export const isSuperAdmin = () => {
  const info = getAdminInfo()
  const roleCodes = info.roleCodes || []
  return info.superAdmin || roleCodes.includes('SUPER_ADMIN') || info.adminId === 1
}

export const hasPermission = (code) => {
  if (!code) return true
  if (isSuperAdmin()) return true
  const codes = getPermissionCodes()
  return codes.includes('*') || codes.includes(code)
}

export const hasAnyPermission = (codes = []) => {
  if (!codes.length) return true
  return codes.some(code => hasPermission(code))
}
