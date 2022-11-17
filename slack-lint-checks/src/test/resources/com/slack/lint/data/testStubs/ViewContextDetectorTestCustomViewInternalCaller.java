package foo;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
public class Example extends TextView {
  public Example(Context context, AttributeSet attrs) {
    super(context, attrs);
  }
  public void bar() {
    Activity a = (Activity) getContext();
  }
}
