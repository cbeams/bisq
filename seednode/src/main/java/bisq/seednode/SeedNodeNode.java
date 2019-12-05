/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.seednode;

import bisq.core.app.BisqEnvironment;
import bisq.core.app.misc.AppSetup;
import bisq.core.app.misc.AppSetupWithP2PAndDAO;
import bisq.core.app.misc.NodeWithP2P;
import bisq.core.app.misc.NodeWithP2PModule;

import bisq.common.UserThread;
import bisq.common.app.BisqModule;
import bisq.common.app.Capabilities;
import bisq.common.app.Capability;
import bisq.common.setup.CommonSetup;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SeedNodeNode extends NodeWithP2P {

    private static final String VERSION = "1.2.3";

    private SeedNodeNode(String... args) {
        super("Bisq Seednode", "bisq-seednode", VERSION, args);
    }

    public static void main(String[] args) {
        log.info("SeedNode.VERSION: " + VERSION);
        BisqEnvironment.setDefaultAppName("bisq_seednode");
        new SeedNodeNode(args).execute();
    }

    @Override
    protected void doExecute() {
        checkMemory(bisqEnvironment, this);
        startShutDownInterval(this);
        CommonSetup.setup(this);
        keepRunning();
    }

    @Override
    protected void addCapabilities() {
        Capabilities.app.addAll(Capability.SEED_NODE);
    }

    @Override
    protected void launchApplication() {
        UserThread.execute(() -> {
            try {
                UserThread.execute(this::onApplicationLaunched);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // We continue with a series of synchronous execution tasks
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected BisqModule getModule() {
        return new NodeWithP2PModule(bisqEnvironment);
    }

    @Override
    protected void startApplication() {
        AppSetup appSetup = injector.getInstance(AppSetupWithP2PAndDAO.class);
        appSetup.start();
    }
}
