import request from '../utils/request'

export const getCategoryList = (params) => request.get('/category/list', { params })
export const addCategory = (data) => request.post('/category', data)
export const updateCategory = (data) => request.put('/category', data)
export const deleteCategory = (id) => request.delete(`/category/${id}`)

export const getBannerList = (params) => request.get('/banner/list', { params })
export const getActiveBanners = (params) => request.get('/banner/active', { params })
export const addBanner = (data) => request.post('/banner', data)
export const updateBanner = (data) => request.put('/banner', data)
export const updateBannerStatus = (id, status) => request.put(`/banner/${id}/status`, null, { params: { status } })
export const batchUpdateBannerStatus = (ids, status) => request.put('/banner/batch-status', ids, { params: { status } })
export const deleteBanner = (id) => request.delete(`/banner/${id}`)
export const batchDeleteBanner = (ids) => request.delete('/banner/batch', { data: ids })

export const getCouponList = (params) => request.get('/coupon/list', { params })
export const getCouponDetail = (id) => request.get(`/coupon/${id}`)
export const addCoupon = (data) => request.post('/coupon', data)
export const updateCoupon = (data) => request.put('/coupon', data)
export const deleteCoupon = (id) => request.delete(`/coupon/${id}`)
export const updateCouponStatus = (id, status) => request.put(`/coupon/${id}/status`, null, { params: { status } })

export const getAfterSaleList = (params) => request.get('/after-sale/list', { params })
export const getAfterSaleDetail = (id) => request.get(`/after-sale/${id}`)
export const getAfterSaleLogistics = (id) => request.get(`/after-sale/${id}/logistics`)
export const getAfterSaleDetailFull = (id) => request.get(`/after-sale/${id}/detail`)
export const handleAfterSale = (data) => request.post('/after-sale/handle', data)

export const getDisputeList = (params) => request.get('/dispute/list', { params })
export const getDisputeDetail = (id) => request.get(`/dispute/${id}`)
export const judgeDispute = (data) => request.post('/dispute/judge', data)

export const getAbnormalOrderList = (params) => request.get('/abnormal/list', { params })
export const getAbnormalDetail = (id) => request.get(`/abnormal/${id}`)
export const handleAbnormal = (data) => request.post('/abnormal/handle', data)

export const getOperationLogList = (params) => request.get('/log/list', { params })

export const getDashboardStats = () => request.get('/dashboard/stats')
export const getOrderTrend = (days) => request.get('/dashboard/order-trend', { params: { days } })
export const getSalesTrend = (days) => request.get('/dashboard/sales-trend', { params: { days } })
export const getCategoryStats = () => request.get('/dashboard/category-stats')
export const getInsights = (handleStatus) => request.get('/dashboard/insights', { params: { handleStatus } })
export const handleInsight = (id, handleStatus) => request.post(`/dashboard/insight/${id}/handle`, null, { params: { handleStatus } })
export const getUserFunnel = () => request.get('/dashboard/user-funnel')
export const getAbnormalDistribution = () => request.get('/dashboard/abnormal-distribution')

// Activity
export const getActivityList = (params) => request.get('/activity/list', { params })
export const getActivityDetail = (id) => request.get(`/activity/${id}`)
export const addActivity = (data) => request.post('/activity', data)
export const updateActivity = (data) => request.put('/activity', data)
export const deleteActivity = (id) => request.delete(`/activity/${id}`)

// Permission
export const getAllPermissions = (module) => request.get('/permission/all', { params: { module } })

// Message Template
export const getMessageTemplateList = () => request.get('/message-template/list')
export const addMessageTemplate = (data) => request.post('/message-template', data)
export const updateMessageTemplate = (data) => request.put('/message-template', data)

// Chat Bot
export const chatBotAsk = (data) => request.post('/chat-bot/ask', data)
export const chatBotFeedback = (data) => request.post('/chat-bot/feedback', data)
export const getChatBotQuickQuestions = () => request.get('/chat-bot/quick-questions')
export const getChatBotHistory = (params) => request.get('/chat-bot/history', { params })

// Notification
export const getNotificationList = (params) => request.get('/notification/list', { params })
export const getNotificationUnreadCount = (params) => request.get('/notification/unread-count', { params })
export const markNotificationRead = (id) => request.put(`/notification/${id}/read`)
export const markAllNotificationRead = (params) => request.put('/notification/read-all', null, { params })
export const deleteNotification = (id) => request.delete(`/notification/${id}`)

// Report
export const getSalesReport = (params) => request.get('/report/sales', { params })
export const getUserAnalysis = (params) => request.get('/report/user-analysis', { params })
export const getGoodsRanking = (params) => request.get('/report/goods-ranking', { params })
export const getMerchantRanking = (params) => request.get('/report/merchant-ranking', { params })
export const getReportOverview = (params) => request.get('/report/overview', { params })
