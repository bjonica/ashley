package ashley.tests.components;

import ashley.core.Component;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class VisualComponent extends Component {
	public final TextureRegion region;

	public VisualComponent(TextureRegion region){
		this.region = region;
	}
}
