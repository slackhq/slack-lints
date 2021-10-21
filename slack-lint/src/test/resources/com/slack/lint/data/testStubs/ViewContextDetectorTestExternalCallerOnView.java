package foo;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
public class Example {
  View view;
  public Example(View v) {
    view = v;
  }
  public void bar() {
    Activity activity = (Activity) view.getContext();
  }
}
