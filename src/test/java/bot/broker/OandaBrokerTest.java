package bot.broker;

import com.google.gson.internal.bind.util.ISO8601Utils;
import org.junit.jupiter.api.Test;

public class OandaBrokerTest {

    @Test
    void exampleTest() {
        OandaBroker b = new OandaBroker();
        System.out.println(b.getAccountBalance());

    }


}
