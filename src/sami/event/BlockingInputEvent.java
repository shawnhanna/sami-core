package sami.event;

/**
 *
 * For blocking input events, we require that all proxies on incoming edges be
 * accounted for (ie, be contained in the RP list of the IE or a copy of it) 
 * For instance, a ProxyExploreArea would compute paths for a set of proxies to
 * take, but each proxy will individually send ProxyPathCompleted IEs to the
 * system 
 * When a gen Blocking IE is received AND it matches the class of a
 * param IE, AND the gen IE has RP, AND the param IE has null RP, check to see
 * if we have created a copy of the param IE with the RP set to this proxy If we
 * have, we will/have match it, and if not, we should create it and set it to
 * fulfilled. Repeat the IE copy process for each proxy token in all the
 * transition's incoming places
 *
 * @author nbb
 */
public class BlockingInputEvent extends InputEvent {
}
