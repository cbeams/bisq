package bisq.seednode;

import bisq.core.app.BisqEnvironment;
import bisq.core.app.misc.AppSetupWithP2PAndDAO;
import bisq.core.app.misc.NodeWithP2PModule;
import bisq.core.locale.CurrencyUtil;
import bisq.core.locale.Res;

import org.springframework.mock.env.MockPropertySource;

import com.google.inject.Guice;

import org.junit.Test;

public class GuiceSetupTest {
    @Test
    public void testGuiceSetup() {
        Res.setup();
        CurrencyUtil.setup();

        NodeWithP2PModule module = new NodeWithP2PModule(new BisqEnvironment(new MockPropertySource()));
        Guice.createInjector(module).getInstance(AppSetupWithP2PAndDAO.class);
    }
}
