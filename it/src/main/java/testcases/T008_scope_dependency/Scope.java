package testcases.T008_scope_dependency;

import javax.inject.Named;

@motif.Scope
public interface Scope {

    String string();

    @Named("a")
    String a();

    @motif.Objects
    class Objects {

        String string(Scope scope) {
            return "s" + scope.a();
        }

        @Named("a")
        String a() {
            return "a";
        }
    }

    @motif.Dependencies
    interface Dependencies {}
}