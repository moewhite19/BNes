package cn.whiteg.bnes.render;

import cn.whiteg.bnes.BNes;
import cn.whiteg.bnes.utils.MapUtils;
import com.grapeshot.halfnes.video.NesColors;

public class BukkitRender1x extends BukkitRender {

    //单个地图的实例
    public BukkitRender1x(String name,BNes plugin) {
        super(name,plugin);
        size = 1;
    }

    @Override
    public void setFrame(int[] frame,int[] bgcolor,boolean dotcrawl) {
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                int index = (y * 2) * 256 + x * 2;
                if (index >= frame.length) continue;
                int rgb = frame[index];
                rgb = NesColors.col[(rgb & 448) >> 6][rgb & 63];
                colors[0][y * 128 + x] = MapUtils.matchColor(rgb);
            }
        }
        renderFps.draw();
    }
}
