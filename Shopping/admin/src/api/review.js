import request from '../utils/request'

export const getReviewList = (params) => request.get('/review/list', { params })
export const replyReview = (id, data) => request.post('/review/reply', { reviewId: id, ...data })
export const updateReviewStatus = (id, data) => request.put(`/review/${id}/status`, data)
export const deleteReview = (id) => request.delete(`/review/${id}`)
