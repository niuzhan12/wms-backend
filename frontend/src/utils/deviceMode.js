// 设备模式状态管理
class DeviceModeManager {
  constructor() {
    // 从localStorage加载保存的模式，如果没有则默认为本地模式
    this.mode = localStorage.getItem('deviceMode') || 'local'
    this.listeners = []
  }

  // 获取当前模式
  getMode() {
    return this.mode
  }

  // 设置模式
  setMode(mode) {
    this.mode = mode
    // 保存到localStorage
    localStorage.setItem('deviceMode', mode)
    this.notifyListeners()
  }

  // 添加监听器
  addListener(callback) {
    this.listeners.push(callback)
  }

  // 移除监听器
  removeListener(callback) {
    const index = this.listeners.indexOf(callback)
    if (index > -1) {
      this.listeners.splice(index, 1)
    }
  }

  // 通知所有监听器
  notifyListeners() {
    this.listeners.forEach(callback => callback(this.mode))
  }

  // 检查是否为远程模式
  isRemoteMode() {
    return this.mode === 'remote'
  }

  // 检查是否为本地模式
  isLocalMode() {
    return this.mode === 'local'
  }
}

// 创建全局实例
const deviceModeManager = new DeviceModeManager()

export default deviceModeManager




