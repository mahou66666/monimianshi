import axios from 'axios'
import { MessageBox, Message } from 'element-ui'
import store from '@/store'
import router from '@/router'
import { getToken } from '@/utils/auth'

let redirectingToLogin = false

// create an axios instance
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API, // url = base url + request url
  // withCredentials: true, // send cookies when cross-domain requests
  timeout: 600000 // request timeout
})

// request interceptor
service.interceptors.request.use(
  config => {
    // do something before request is sent

    if (store.getters.token) {
      // let each request carry token
      // ['X-Token'] is a custom headers key
      // please modify it according to the actual situation
      config.headers['X-Token'] = getToken()
    }
    return config
  },
  error => {
    // do something with request error
    console.log(error) // for debug
    return Promise.reject(error)
  }
)

// response interceptor
service.interceptors.response.use(
  /**
   * If you want to get http information such as headers or status
   * Please return  response => response
  */

  /**
   * Determine the request status by custom code
   * Here is just an example
   * You can also judge the status by HTTP Status Code
   */
  response => {
    const res = response.data

    // if the custom code is not 20000 or 200, it is judged as an error.
    if (res.code !== 20000 && res.code !== 200) {
      const silentErrorMessage = response.config && response.config.silentErrorMessage
      if (!silentErrorMessage) {
        Message({
          message: res.message || 'Error',
          type: 'error',
          duration: 5 * 1000
        })
      }

      if (res.message && String(res.message).includes('登录已过期')) {
        if (!redirectingToLogin) {
          redirectingToLogin = true
          store.dispatch('user/resetToken').then(() => {
            const redirect = router.currentRoute && router.currentRoute.fullPath ? router.currentRoute.fullPath : '/'
            const query = { redirect }
            router.replace({ path: '/login', query })
          }).finally(() => {
            setTimeout(() => {
              redirectingToLogin = false
            }, 300)
          })
        }
        return Promise.reject(new Error(res.message || 'Error'))
      }

      // 50008: Illegal token; 50012: Other clients logged in; 50014: Token expired;
      if (res.code === 40100 || res.code === 50008 || res.code === 50012 || res.code === 50014) {
        const shouldRedirect = res.code === 40100
        if (shouldRedirect) {
          if (!redirectingToLogin) {
            redirectingToLogin = true
            store.dispatch('user/resetToken').then(() => {
              const redirect = router.currentRoute && router.currentRoute.fullPath ? router.currentRoute.fullPath : '/'
              const query = { redirect }
              router.replace({ path: '/login', query })
            }).finally(() => {
              setTimeout(() => {
                redirectingToLogin = false
              }, 300)
            })
          }
        } else {
          MessageBox.confirm('You have been logged out, you can cancel to stay on this page, or log in again', 'Confirm logout', {
            confirmButtonText: 'Re-Login',
            cancelButtonText: 'Cancel',
            type: 'warning'
          }).then(() => {
            store.dispatch('user/resetToken').then(() => {
              location.reload()
            })
          })
        }
      }
      return Promise.reject(new Error(res.message || 'Error'))
    } else {
      return res
    }
  },
  error => {
    console.log('err' + error) // for debug
    const silentErrorMessage = error.config && error.config.silentErrorMessage
    if (!silentErrorMessage) {
      Message({
        message: error.message,
        type: 'error',
        duration: 5 * 1000
      })
    }
    return Promise.reject(error)
  }
)

export default service
