package foo;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
public class Example {
  TextView view;
  public Example(TextView v) {
    view = v;
  }
  public void bar() {
    Activity activity = (Activity) view.getContext();
  }
}
