#!/usr/bin/env python3
"""
Modbus TCP 服务器模拟器
用于测试WMS系统的Modbus连接
"""

import socket
import threading
import time
import struct

class ModbusTCPServer:
    def __init__(self, host='127.0.0.1', port=502, slave_id=1):
        self.host = host
        self.port = port
        self.slave_id = slave_id
        self.socket = None
        self.running = False
        self.registers = [0] * 100  # 模拟100个寄存器
        
        # 初始化一些默认值
        self.registers[0] = 1   # 4001: WMS模式
        self.registers[1] = 0   # 4002: WMS忙碌状态
        self.registers[2] = 0   # 4003: WMS出库进度
        self.registers[3] = 0   # 4004: WMS入库进度
        self.registers[4] = 0   # 4005: WMS出库完成
        self.registers[5] = 0   # 4006: WMS入库完成
        self.registers[6] = 0   # 4007: MES出库订单
        self.registers[7] = 0   # 4008: MES入库订单
        self.registers[8] = 0   # 4009: WMS当前行
        self.registers[9] = 0   # 4010: WMS当前列
        
    def start(self):
        """启动Modbus TCP服务器"""
        try:
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.socket.bind((self.host, self.port))
            self.socket.listen(5)
            self.running = True
            
            print(f"🚀 Modbus TCP服务器已启动")
            print(f"📍 地址: {self.host}:{self.port}")
            print(f"🆔 从站ID: {self.slave_id}")
            print(f"📊 寄存器数量: {len(self.registers)}")
            print("=" * 50)
            
            while self.running:
                try:
                    client_socket, address = self.socket.accept()
                    print(f"🔗 新连接来自: {address}")
                    
                    # 为每个客户端创建处理线程
                    client_thread = threading.Thread(
                        target=self.handle_client,
                        args=(client_socket, address)
                    )
                    client_thread.daemon = True
                    client_thread.start()
                    
                except socket.error as e:
                    if self.running:
                        print(f"❌ 接受连接时出错: {e}")
                        
        except Exception as e:
            print(f"❌ 启动服务器失败: {e}")
        finally:
            self.stop()
    
    def handle_client(self, client_socket, address):
        """处理客户端连接"""
        try:
            while self.running:
                # 接收Modbus TCP请求
                data = client_socket.recv(1024)
                if not data:
                    break
                
                if len(data) < 8:  # Modbus TCP最小头部长度
                    continue
                
                # 解析Modbus TCP头部
                transaction_id = struct.unpack('>H', data[0:2])[0]
                protocol_id = struct.unpack('>H', data[2:4])[0]
                length = struct.unpack('>H', data[4:6])[0]
                unit_id = data[6]
                function_code = data[7]
                
                print(f"📨 收到请求 - 事务ID: {transaction_id}, 功能码: {function_code}, 单元ID: {unit_id}")
                
                # 处理不同的功能码
                if function_code == 3:  # 读保持寄存器
                    response = self.handle_read_holding_registers(data, transaction_id)
                elif function_code == 6:  # 写单个寄存器
                    response = self.handle_write_single_register(data, transaction_id)
                else:
                    # 不支持的功能码
                    response = self.create_error_response(transaction_id, unit_id, 1)  # 非法功能码
                
                if response:
                    client_socket.send(response)
                    print(f"📤 发送响应 - 长度: {len(response)} 字节")
                
        except Exception as e:
            print(f"❌ 处理客户端 {address} 时出错: {e}")
        finally:
            client_socket.close()
            print(f"🔌 客户端 {address} 已断开")
    
    def handle_read_holding_registers(self, data, transaction_id):
        """处理读保持寄存器请求"""
        if len(data) < 12:
            return None
        
        start_address = struct.unpack('>H', data[8:10])[0]
        quantity = struct.unpack('>H', data[10:12])[0]
        
        print(f"📖 读保持寄存器 - 起始地址: {start_address}, 数量: {quantity}")
        
        # 检查地址范围
        if start_address + quantity > len(self.registers):
            return self.create_error_response(transaction_id, self.slave_id, 2)  # 非法数据地址
        
        # 构建响应
        response_data = bytearray()
        response_data.extend(struct.pack('>H', transaction_id))  # 事务ID
        response_data.extend(struct.pack('>H', 0))  # 协议ID
        response_data.extend(struct.pack('>H', 3 + quantity * 2))  # 长度
        response_data.append(self.slave_id)  # 单元ID
        response_data.append(3)  # 功能码
        response_data.append(quantity * 2)  # 字节数
        
        # 添加寄存器数据
        for i in range(quantity):
            value = self.registers[start_address + i]
            response_data.extend(struct.pack('>H', value))
        
        return bytes(response_data)
    
    def handle_write_single_register(self, data, transaction_id):
        """处理写单个寄存器请求"""
        if len(data) < 12:
            return None
        
        address = struct.unpack('>H', data[8:10])[0]
        value = struct.unpack('>H', data[10:12])[0]
        
        print(f"✏️ 写单个寄存器 - 地址: {address}, 值: {value}")
        
        # 检查地址范围
        if address >= len(self.registers):
            return self.create_error_response(transaction_id, self.slave_id, 2)  # 非法数据地址
        
        # 更新寄存器值
        self.registers[address] = value
        
        # 构建响应（回显请求）
        response_data = bytearray()
        response_data.extend(struct.pack('>H', transaction_id))  # 事务ID
        response_data.extend(struct.pack('>H', 0))  # 协议ID
        response_data.extend(struct.pack('>H', 6))  # 长度
        response_data.append(self.slave_id)  # 单元ID
        response_data.append(6)  # 功能码
        response_data.extend(struct.pack('>H', address))  # 地址
        response_data.extend(struct.pack('>H', value))  # 值
        
        return bytes(response_data)
    
    def create_error_response(self, transaction_id, unit_id, error_code):
        """创建错误响应"""
        response_data = bytearray()
        response_data.extend(struct.pack('>H', transaction_id))  # 事务ID
        response_data.extend(struct.pack('>H', 0))  # 协议ID
        response_data.extend(struct.pack('>H', 3))  # 长度
        response_data.append(unit_id)  # 单元ID
        response_data.append(0x80 + 3)  # 功能码 + 错误标志
        response_data.append(error_code)  # 错误码
        
        return bytes(response_data)
    
    def stop(self):
        """停止服务器"""
        self.running = False
        if self.socket:
            self.socket.close()
        print("🛑 Modbus TCP服务器已停止")
    
    def print_registers(self):
        """打印当前寄存器状态"""
        print("\n📊 当前寄存器状态:")
        for i in range(0, min(20, len(self.registers)), 5):
            line = ""
            for j in range(5):
                if i + j < len(self.registers):
                    line += f"400{i+j+1:02d}:{self.registers[i+j]:3d} "
            print(line)
        print()

def main():
    print("🔧 Modbus TCP 服务器模拟器")
    print("=" * 50)
    
    # 创建两个服务器实例
    servers = []
    
    # MES-WMS服务器 (端口502)
    mes_wms_server = ModbusTCPServer('127.0.0.1', 502, 1)
    servers.append(mes_wms_server)
    
    # WMS-堆垛机服务器 (端口503)
    stacker_server = ModbusTCPServer('127.0.0.1', 503, 1)
    servers.append(stacker_server)
    
    try:
        # 启动服务器线程
        threads = []
        for server in servers:
            thread = threading.Thread(target=server.start)
            thread.daemon = True
            thread.start()
            threads.append(thread)
        
        print("✅ 所有服务器已启动")
        print("💡 按 Ctrl+C 停止服务器")
        
        # 定期打印寄存器状态
        while True:
            time.sleep(10)
            for i, server in enumerate(servers):
                port = 502 + i
                print(f"\n🔍 端口 {port} 服务器状态:")
                server.print_registers()
                
    except KeyboardInterrupt:
        print("\n🛑 正在停止服务器...")
        for server in servers:
            server.stop()
        print("✅ 所有服务器已停止")

if __name__ == "__main__":
    main()

