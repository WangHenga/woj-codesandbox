import java.security.Permission;

public class MySecurityManager extends SecurityManager{
    @Override
    public void checkPermission(Permission perm) {
    }
//    @Override
//    public void checkRead(String file) {
//        throw new RuntimeException("checkRead权限不足:"+file);
//    }

    @Override
    public void checkExec(String cmd) {
        throw new RuntimeException("checkExec权限不足:"+cmd);
    }

    @Override
    public void checkWrite(String file) {
        throw new RuntimeException("checkWrite权限不足:"+file);
    }

    @Override
    public void checkConnect(String host, int port) {
        throw new RuntimeException("checkConnect权限不足:"+host+":"+port);
    }
}
