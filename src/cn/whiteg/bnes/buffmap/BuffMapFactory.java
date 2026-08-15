package cn.whiteg.bnes.buffmap;

/**
 * 地图更新器工厂，根据 UpdateMode 配置创建对应的 {@link BuffMapConstructor} 实例
 * <p>
 * 支持的格式：
 * <pre>
 * none            - 不进行任何检测，直接发送完整画面
 * scan / scanning - 扫描整个画面，将全部变化区域包围成一个矩形发送
 * chunk[:N]       - 将画面划分为 NxN 固定网格，逐块检测发送（默认 N=8）
 * cas[:N]         - 在 NxN 网格内再做矩形裁剪（默认 N=8）
 * smart[:块大小[:每帧预算]] - 智能脏矩形检测，自动聚合+拆分变化区域（默认块大小=8）
 * </pre>
 * 推荐使用 <b>smart</b>。例如：
 * <ul>
 *   <li>"smart" 使用默认 8x8 像素块检测</li>
 *   <li>"smart:4" 使用 4x4 像素块，检测更精细</li>
 *   <li>"smart:8:2048" 同时限制每帧最多发送 2048 字节，超出部分下一帧发送</li>
 * </ul>
 */
public class BuffMapFactory {
    public static BuffMapConstructor create(String mode) {
        return create(mode,0);
    }

    /**
     * @param mode              更新模式，见类注释
     * @param maxBytesPerFrame  每帧发送预算(字节)，仅对 smart 模式生效，0为不限
     */
    public static BuffMapConstructor create(String mode,int maxBytesPerFrame) {
        if (mode == null || mode.isBlank()) mode = "smart";
        mode = mode.trim().toLowerCase();
        String name = mode;
        int param = 8;
        int budget = maxBytesPerFrame;
        int idx = mode.indexOf(':');
        if (idx > 0){
            name = mode.substring(0,idx);
            String[] params = mode.substring(idx + 1).split(":");
            param = parseInt(params[0],8);
            if (params.length > 1) budget = parseInt(params[1],maxBytesPerFrame);
        }
        try{
            switch (name){
                case "none":
                case "full":
                    return new NoneConstructor();
                case "scan":
                case "scanning":
                    return new ScanningConstructor();
                case "chunk":
                    return new ChunkConstructor(param);
                case "cas":
                case "c&s":
                case "cands":
                    return new CAndSConstructor(param);
                case "smart":
                case "auto":
                default:
                    return new SmartConstructor(param,budget);
            }
        }catch (IllegalArgumentException e){
            //参数非法时回退到默认smart,避免配置错误导致游戏机无法初始化
            return new SmartConstructor(8,budget);
        }
    }

    private static int parseInt(String str,int def) {
        try{
            return Integer.parseInt(str.trim());
        }catch (NumberFormatException e){
            return def;
        }
    }
}
