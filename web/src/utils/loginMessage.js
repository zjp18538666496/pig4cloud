import { ElMessageBox } from 'element-plus'

let isShowingLoginMessageBox = false

const showLoginMessageBox = async () => {
    if (isShowingLoginMessageBox) return Promise.resolve(false)
    isShowingLoginMessageBox = true
    try {
        return await ElMessageBox.confirm('您还没有登录，请先登录', '提示', {
            confirmButtonText: '去登录',
            cancelButtonText: '取消',
            type: 'warning',
        })
            .then(() => {
                return true
            })
            .catch(() => {
                return false
            })
    } finally {
        isShowingLoginMessageBox = false
    }
}

export { showLoginMessageBox }
