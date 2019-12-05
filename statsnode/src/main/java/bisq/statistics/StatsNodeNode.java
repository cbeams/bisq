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

package bisq.statistics;

import bisq.core.app.BisqEnvironment;
import bisq.core.app.misc.NodeWithP2P;
import bisq.core.app.misc.NodeWithP2PModule;

import bisq.common.UserThread;
import bisq.common.app.BisqModule;
import bisq.common.setup.CommonSetup;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StatsNodeNode extends NodeWithP2P {

    private static final String VERSION = "1.0.1";
    private Statistics statistics;

    private StatsNodeNode(String[] args) {
        super("Bisq Statsnode", "bisq-statsnode", VERSION, args);
    }

    public static void main(String[] args) {
        log.info("Statistics.VERSION: " + VERSION);
        BisqEnvironment.setDefaultAppName("bisq_statistics");
        new StatsNodeNode(args).execute();
    }

    @Override
    protected void doExecute() {
        checkMemory(bisqEnvironment, this);
        CommonSetup.setup(this);
        keepRunning();
    }

    @Override
    protected void launchApplication() {
        UserThread.execute(() -> {
            try {
                statistics = new Statistics();
                UserThread.execute(this::onApplicationLaunched);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void onApplicationLaunched() {
        super.onApplicationLaunched();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // We continue with a series of synchronous execution tasks
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected BisqModule getModule() {
        return new NodeWithP2PModule(bisqEnvironment);
    }

    @Override
    protected void applyInjector() {
        super.applyInjector();

        statistics.setInjector(injector);
    }

    @Override
    protected void startApplication() {
        statistics.startApplication();
    }
}
