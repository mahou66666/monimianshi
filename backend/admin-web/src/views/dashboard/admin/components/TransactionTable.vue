<template>
  <el-table :data="list" style="width: 100%;padding-top: 15px;">
    <el-table-column label="Order_No" min-width="200">
      <template slot-scope="scope">
        {{ scope.row.order_no | orderNoFilter }}
      </template>
    </el-table-column>
    <el-table-column label="Price" width="195" align="center">
      <template slot-scope="scope">
        ¥{{ scope.row.price | toThousandFilter }}
      </template>
    </el-table-column>
    <el-table-column label="Status" width="100" align="center">
      <template slot-scope="{row}">
        <el-tag :type="row.status | statusFilter">
          {{ row.status }}
        </el-tag>
      </template>
    </el-table-column>
  </el-table>
</template>

<script>
const defaultTransactions = [
  { order_no: 'IE-20260322-0001', price: 4250, status: 'success' },
  { order_no: 'IE-20260322-0002', price: 2890, status: 'pending' },
  { order_no: 'IE-20260322-0003', price: 1990, status: 'success' },
  { order_no: 'IE-20260322-0004', price: 3650, status: 'success' },
  { order_no: 'IE-20260322-0005', price: 1580, status: 'pending' },
  { order_no: 'IE-20260322-0006', price: 5300, status: 'success' },
  { order_no: 'IE-20260322-0007', price: 2460, status: 'success' },
  { order_no: 'IE-20260322-0008', price: 1720, status: 'pending' }
]

export default {
  filters: {
    statusFilter(status) {
      const statusMap = {
        success: 'success',
        pending: 'danger'
      }
      return statusMap[status]
    },
    orderNoFilter(str) {
      return str.substring(0, 30)
    }
  },
  data() {
    return {
      // Keep dashboard usable after removing vue-element-admin mock endpoints.
      list: defaultTransactions
    }
  }
}
</script>
