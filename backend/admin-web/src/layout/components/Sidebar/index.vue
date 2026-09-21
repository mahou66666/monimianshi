<template>
  <div class="sidebar-container" :class="{'has-logo':showLogo}">
    <logo v-if="showLogo" :collapse="isCollapse" />
    <el-scrollbar wrap-class="scrollbar-wrapper">
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :background-color="variables.menuBg"
        :text-color="variables.menuText"
        :unique-opened="false"
        :active-text-color="variables.menuActiveText"
        :collapse-transition="false"
        mode="vertical"
      >
        <sidebar-item v-for="route in sidebar_routes" :key="route.path" :item="route" :base-path="route.path" />
      </el-menu>
    </el-scrollbar>

    <div class="sidebar-footer">
      <el-button class="more-btn" size="mini" plain @click="openLegacyDialog">
        <i class="el-icon-more" />
        <span v-if="!isCollapse">更多功能</span>
      </el-button>
    </div>

    <el-dialog title="更多功能" :visible.sync="legacyDialogVisible" width="720px" append-to-body>
      <el-input v-model="legacyKeyword" placeholder="搜索页面" clearable />
      <div class="legacy-list">
        <div v-for="item in filteredLegacyItems" :key="item.path" class="legacy-item">
          <el-button type="text" @click="goLegacy(item.path)">{{ item.title }}</el-button>
          <span class="legacy-path">{{ item.path }}</span>
        </div>
        <div v-if="filteredLegacyItems.length === 0" class="legacy-empty">暂无可用页面</div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import path from 'path'
import { mapGetters } from 'vuex'
import Logo from './Logo'
import SidebarItem from './SidebarItem'
import variables from '@/styles/variables.scss'

export default {
  components: { SidebarItem, Logo },
  data() {
    return {
      legacyDialogVisible: false,
      legacyKeyword: ''
    }
  },
  computed: {
    ...mapGetters([
      'sidebar_routes',
      'legacy_routes',
      'sidebar'
    ]),
    legacyItems() {
      const items = this.flattenRoutes(this.legacy_routes || [])
      const map = new Map()
      items.forEach((it) => {
        if (!map.has(it.path)) map.set(it.path, it)
      })
      return Array.from(map.values())
    },
    filteredLegacyItems() {
      const kw = String(this.legacyKeyword || '').trim().toLowerCase()
      if (!kw) return this.legacyItems
      return this.legacyItems.filter((it) => String(it.title || '').toLowerCase().includes(kw) || String(it.path).toLowerCase().includes(kw))
    },
    activeMenu() {
      const route = this.$route
      const { meta, path } = route
      // if set path, the sidebar will highlight the path you set
      if (meta.activeMenu) {
        return meta.activeMenu
      }
      return path
    },
    showLogo() {
      return this.$store.state.settings.sidebarLogo
    },
    variables() {
      return variables
    },
    isCollapse() {
      return !this.sidebar.opened
    }
  },
  watch: {
    $route() {
      if (this.legacyDialogVisible) {
        this.legacyDialogVisible = false
      }
    }
  },
  methods: {
    resolvePath(basePath, routePath) {
      return path.resolve(basePath || '', routePath || '')
    },
    flattenRoutes(routes, basePath = '') {
      const res = []
      ;(routes || []).forEach((r) => {
        if (!r || r.hidden) return
        const fullPath = this.resolvePath(basePath, r.path)
        if (r.children && r.children.length > 0) {
          res.push(...this.flattenRoutes(r.children, fullPath))
        } else if (r.meta && r.meta.title) {
          res.push({ path: fullPath, title: r.meta.title })
        }
      })
      return res
    },
    openLegacyDialog() {
      this.legacyDialogVisible = true
      this.legacyKeyword = ''
    },
    goLegacy(path) {
      this.legacyDialogVisible = false
      this.$router.push(path)
    }
  }
}
</script>

<style scoped>
.sidebar-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

::v-deep .scrollbar-wrapper {
  flex: 1;
}

.sidebar-footer {
  padding: 8px 10px 10px 10px;
}

.more-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.legacy-list {
  margin-top: 10px;
  max-height: 420px;
  overflow: auto;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.legacy-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid #ebeef5;
}

.legacy-item:last-child {
  border-bottom: none;
}

.legacy-path {
  color: #909399;
  font-size: 12px;
  margin-left: 16px;
}

.legacy-empty {
  padding: 14px 12px;
  color: #909399;
  text-align: center;
}
</style>
