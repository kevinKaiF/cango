# cango
cango的作用是将canal和yuong整合到一个系统里，可以同步mysql数据库也可以同步oracle数据库。

# cango结构
![](https://github.com/kevinKaiF/cango/blob/master/cango-example/src/doc/image/cango_architecture.png)

简单说明一下，将需要同步的数据库信息注册到cangoManagerService，数据类型为Mysql则启动canal实例，否则启动yugong实例。
最后将同步到的数据发送到kafka。另外，cango能够注册多个canal实例和yugong实例。

# TODO
1. 每一个canal实例和yugong实例有name作为唯一识别标志，目前使用host和port拼接作为默认的name，是否需要让使用者自己定义name？
2. yugong实例目前只支持增量，以后会增加全量

