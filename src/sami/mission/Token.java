package sami.mission;

import com.perc.mitpas.adi.mission.planning.task.Task;
import sami.mission.TokenSpecification.TokenType;
import sami.proxy.ProxyInt;

/**
 *
 * @author pscerri
 */
public class Token {

    private final String name;
    private ProxyInt proxy;
    private final Task task;
    private final TokenType type;

    public Token(String name, TokenType type, ProxyInt proxy, Task task) {
        this.name = name;
        this.proxy = proxy;
        this.task = task;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public TokenType getType() {
        return type;
    }

    public Task getTask() {
        return task;
    }

    public ProxyInt getProxy() {
        return proxy;
    }

    public void setProxy(ProxyInt proxy) {
        this.proxy = proxy;
    }

    @Override
    public String toString() {
        String ret = "Token:";
        if (type == TokenType.Proxy) {
            ret += "P-" + (proxy == null ? "NULL" : proxy.getProxyId()) + "-" + (task == null ? "NULL" : task.getName());
        } else if (type == TokenType.Task) {
            ret += "T-" + (proxy == null ? "NULL" : proxy.getProxyId()) + "-" + (task == null ? "NULL" : task.getName());
        } else {
            ret += type.toString();
        }
        return ret;
    }

    @Override
    public Token clone() {
        return new Token(name, type, proxy, task);
    }
}
