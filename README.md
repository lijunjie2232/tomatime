# Tomato Timer (番茄时钟)

一个简单易用的番茄工作法计时器应用。

## 功能特点

- 🍅 专注计时（默认25分钟）
- 😴 短休息（5分钟）
- 🛌 长休息（15分钟）
- ⚙️ 可自定义专注时间
- 🎨 简洁美观的 Material Design 界面

## 使用方法

1. 点击"开始"按钮开始专注计时
2. 点击"暂停"按钮暂停计时
3. 点击"重置"按钮重置计时器
4. 使用底部按钮切换不同模式：
   - 专注时间：用于工作或学习
   - 短休息：短暂放松
   - 长休息：较长时间休息

## 自定义设置

在专注模式下，点击时间显示区域可以打开设置面板，自定义专注时间长度。

## 构建与发布

本项目使用 GitHub Actions 自动化构建和发布：
- 每次推送到 main 分支时自动构建
- 自动生成预发布版本

### 配置 GitHub Secrets（用于签名 APK）

为了安全地签名发布版本的 APK，您需要在 GitHub 仓库中配置以下 Secrets：

1. 进入仓库的 Settings 页面
2. 点击左侧的 Secrets and variables > Actions
3. 点击 "New repository secret" 按钮添加以下 secrets：

| Secret Name | Description | 示例值 |
|-------------|-------------|--------|
| SIGNING_KEYSTORE | Base64 编码的 keystore 文件 | (通过 `base64 keystore.jks` 获取) |
| SIGNING_KEY_ALIAS | 签名密钥别名 | key0 |
| SIGNING_KEY_PASSWORD | 密钥密码 | 123456 |
| SIGNING_STORE_PASSWORD | keystore 密码 | 123456 |

#### 生成 keystore 并转换为 Base64

如果您还没有 keystore 文件，可以使用以下命令创建一个：

```bash
keytool -genkey -v -keystore release.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000
```

然后将 keystore 文件转换为 Base64 格式：

```bash
base64 release.keystore > keystore.base64
```

将 keystore.base64 文件的**内容**复制到 SIGNING_KEYSTORE secret 中。

确保在 GitHub 仓库中配置以下 Secrets：
- SIGNING_KEYSTORE：Base64 编码的 keystore 文件
- SIGNING_KEY_ALIAS：签名密钥别名
- SIGNING_KEY_PASSWORD：密钥密码
- SIGNING_STORE_PASSWORD：keystore 密码