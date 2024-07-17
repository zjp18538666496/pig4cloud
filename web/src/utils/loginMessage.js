import { ElMessageBox } from 'element-plus'

let isShowingLoginMessageBox = false

const showLoginMessageBox = async () => {
    if (isShowingLoginMessageBox) return
    isShowingLoginMessageBox = true
    try {
        await ElMessageBox.confirm('您还没有登录，请先登录', '提示', {
            confirmButtonText: '去登录',
            cancelButtonText: '取消',
            type: 'warning',
        })
    } finally {
        isShowingLoginMessageBox = false
    }
}

export { showLoginMessageBox }
