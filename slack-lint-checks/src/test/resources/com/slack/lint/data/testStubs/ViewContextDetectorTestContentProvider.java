package foo;
import android.content.ContentProvider;
import android.content.Context;
public abstract class DummyProvider extends ContentProvider {
  public void bar() {
    Context c = getContext();
  }
}
