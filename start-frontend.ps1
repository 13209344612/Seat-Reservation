# Windows PowerShell 启动脚本
# 用于快速安装依赖并启动前端开发服务器
# 注意: 首次运行可能需要执行: Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  座位预约系统 - 前端启动脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 检查 Node.js
Write-Host "检查 Node.js..." -ForegroundColor Yellow
$nodeVersion = node --version 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "错误: 未找到 Node.js，请先安装 Node.js" -ForegroundColor Red
    exit 1
}
Write-Host "Node.js 版本: $nodeVersion" -ForegroundColor Green

# 检查 npm
Write-Host "检查 npm..." -ForegroundColor Yellow
$npmVersion = npm --version 2>&1
Write-Host "npm 版本: $npmVersion" -ForegroundColor Green
Write-Host ""

# 进入 frontend 目录
Set-Location $PSScriptRoot\frontend

Write-Host "开始安装依赖..." -ForegroundColor Yellow
Write-Host ""

# 安装依赖
npm install

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  依赖安装完成！" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "正在启动开发服务器..." -ForegroundColor Yellow
    Write-Host ""
    
    # 启动开发服务器
    npm run dev
} else {
    Write-Host ""
    Write-Host "错误: 依赖安装失败" -ForegroundColor Red
    Write-Host "请检查网络连接或手动运行: npm install" -ForegroundColor Yellow
}
