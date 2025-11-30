# AxSellwands - CustomFishing & ExcellentShop Fork

> 本 Fork 版本添加了对 CustomFishing 和 ExcellentShop 的完整支持

## 🎯 Fork 新增特性

### 多价格提供商系统
- ✅ 同时支持 CustomFishing 和 ExcellentShop 作为价格提供商
- ✅ 智能优先级系统：CustomFishing (优先级 100) > ExcellentShop (优先级 50)
- ✅ 自动选择价格最高的提供商
- ✅ 智能回退机制：当一个提供商不可用时自动切换到另一个

### 完整的限制支持
- ✅ **CustomFishing**: 遵守每日出售金额限制，自动扣除玩家收益额度
- ✅ **ExcellentShop**: 遵守服务器全局和玩家个人库存限制，检查权限和等级要求
- ✅ 达到限额后自动尝试使用备用提供商

### 配置方法

在 `hooks.yml` 中配置：

```yaml
hooks:
  price-plugin: "MULTI"  # 启用多价格提供商模式
```

或使用显式配置：

```yaml
hooks:
  price-plugin: "CUSTOMFISHING+EXCELLENTSHOP"
```

### 工作原理

1. **优先级检测**: 检测为 CustomFishing 物品时，优先通过 CustomFishing 出售
2. **智能回退**: 如果 CustomFishing 达到每日限额，自动使用 ExcellentShop 出售
3. **价格比较**: 当两个插件都有价格时，自动选择价格最高的
4. **限制遵守**: 完全遵守两个插件的所有限制规则

### 技术实现

**新增类**:
- `AdvancedPricesHook` - 高级价格钩子接口
- `SellResult` - 出售结果类
- `CustomFishingHook` - CustomFishing 集成
- `MultiPricesHook` - 多价格提供商管理器

**修改类**:
- `ExcellentShopHook` - 增加限制检查
- `HookManager` - 支持多价格提供商
- `SellwandUseListener` - 使用新的价格系统

### 依赖要求

- CustomFishing API 2.3.16+
- ExcellentShop 4.20.0+
- Java 21

### 启动日志示例

成功加载两个插件：
```
[AxSellwands] Hooked into CustomFishing! (Priority: 100)
[AxSellwands] Hooked into ExcellentShop! (Priority: 50)
[AxSellwands] Using multi-provider mode with 2 provider(s)!
```

---

## 原版信息

**Bug Reports and Feature Requests:** https://github.com/Artillex-Studios/Issues

**Support:** https://dc.artillex-studios.com/

<img width="1920" height="1080" alt="axsellwands-banner" src="https://github.com/user-attachments/assets/8ec7da9b-9e9c-485e-b284-f22b966a5025" />
