# BeautifulNes Minecraft服务器Nes模拟器插件
<img width="160" src="icon.png" alt="logo"></br>
## 基于 halfnes 开发:<https://github.com/andrew-hoffman/halfnes>

### 指令及权限
* /bnes: 查看插件帮助(仅显示有权限使用的指令)
* /bnes reload: 重载配置 - 权限bnes.command.reload
* /bnes create <实例名>: 用指定实例名创建游戏机 - bnes.command.create (最后加个min参数可获得单地图游戏机)
* /bnes map <实例名> [给与玩家:默认自己] : 获取指定实例的地图 - bnes.command.map
* /bnes close <实例名>: 关闭指定游戏机 - bnes.command.close
* /bnes showfps: 开关fps显示 - bnes.command.showfps
* /bnes rename <实例名> <新实例名>: 重命名实例 - bnes.command.rename
* /bnes card <卡带名称>: 获取指定卡带 - bnes.command.card
* /bnes menu [页面:默认1]: 卡带菜单 - bnes.command.menu

### 食用方法
    需要椅子插件配合使用，没有椅子插件可以用矿车代替
    将地图挂在墙上后，坐在椅子上右键地图可加入游戏。
    最多支持2个玩家同时游玩
######
    首次运行会自动在插件数据目录创建roms文件夹，将需要运行的游戏丢进roms文件夹
    会自动在插件目录创建non_card.nes文件，当创建新游戏机以及加载的rom无效时会加载这个测试rom，有需要可以自行替换。
    听到游戏声音需要安装voicechat模组和插件
		
### 默认按键映射(可通过配置文件修改)

    游戏移动方向 > 手柄方向
    跳跃 > 手柄A
    交换副手 > 手柄B
    左键地图 > select键
    右键地图 > start键
    “start”和“A”同时按下 > 重置游戏(受按键映射配置影响)

## Download
~~McBBS: https://www.mcbbs.net/thread-1267926-1-1.html~~

Releases: https://github.com/moewhite19/BNes/releases