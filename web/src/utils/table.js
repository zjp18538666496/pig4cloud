import { getType } from '@/utils/getDataType.js'

class BaseTable {
    table = {
        // 表格数据
        rows: [],
        // 表格高度
        height: window.innerHeight - 50 - 30 - 40 - 52 - 52,
        // 查询条件
        query: {
            page: 1,
            pageSize: 10,
        },
        // 表格分页组件属性
        pagination: {
            total: 0,
            disabled: false,
            background: false,
            layout: 'total, sizes, prev, pager, next, jumper',
            pageSize: [10, 20, 30, 40, 50],
        },
    }

    constructor(data) {
        if (getType(data) === 'object') {
            const { rows, height, query, pagination } = data
            rows && (this.table.rows = data.rows)
            height && (this.table.height = data.height)
            query && (this.table.query = data.query)
            pagination && (this.table.pagination = data.pagination)
        }
    }
}

export default BaseTable
