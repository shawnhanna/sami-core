package sami.mission;

/**
 *
 * @author nbb
 */
public class TokenSpecification implements java.io.Serializable {

    static final long serialVersionUID = 2L;
    private final String name;
    // There are 3 "types" of tokens
    //  Generic tokens do not have a proxy or task associated with them
    //  Proxy tokens have a non-specific proxy, but no task, associated with them
    //  Task tokens have a specific task and potentially a proxy associated with them

    public enum TokenType {

        Task, Proxy,
        MatchNoReq, MatchRelevantProxy, MatchGeneric,
        TakeAll, CopyRelevantTask, CopyRelevantProxy, TakeRelevantTask, TakeRelevantProxy, TakeGeneric, TakeNone, AddGeneric, ConsumeRelevantTask, ConsumeRelevantProxy, ConsumeGeneric, TakeTask, TakeProxy
    };
    private TokenType type;
    String taskClassName;

    public TokenSpecification(String name, TokenType type, String taskClassName) {
        this.name = name;
        this.taskClassName = taskClassName;
        this.type = type;
    }

    public TokenType getType() {
        return type;
    }

    public void setTokenType(TokenType tokenType) {
        this.type = tokenType;
    }

    public String getName() {
        return name;
    }

    public String getTaskClassName() {
        return taskClassName;
    }

    @Override
    public String toString() {
        return name;
    }

    // Need to do this because we decided not to serialize the TokenSpecifications
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof TokenSpecification)) {
            return false;
        }

        TokenSpecification tokenSpec2 = (TokenSpecification) obj;
        if (type == tokenSpec2.type) {
            switch (type) {
                case Task:
                    if (name.equals(tokenSpec2.name)
                            && taskClassName.equals(tokenSpec2.taskClassName)) {
                        return true;
                    }
                    return false;
                default:
                    return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 79 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 79 * hash + (this.taskClassName != null ? this.taskClassName.hashCode() : 0);
        return hash;
    }
}
