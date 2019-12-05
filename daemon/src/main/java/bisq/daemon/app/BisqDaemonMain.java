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

package bisq.daemon.app;

import bisq.core.app.BisqExecutable;
import bisq.core.app.BisqSetup;
import bisq.core.app.CoreModule;
import bisq.core.grpc.CoreApi;
import bisq.core.trade.TradeManager;

import bisq.common.UserThread;
import bisq.common.app.AppModule;
import bisq.common.app.Version;
import bisq.common.setup.CommonSetup;
import bisq.common.storage.CorruptedDatabaseFilesHandler;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqDaemonMain extends BisqExecutable {

    private BisqDaemon bisqDaemon;

    private BisqDaemonMain(String[] args) {
        super("Bisq Daemon", "bisqd", Version.VERSION, args);
    }

    public static void main(String[] args) {
        new BisqDaemonMain(args).execute();
    }

    @Override
    protected void configUserThread() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(this.getClass().getSimpleName())
                .setDaemon(false) // keep daemon process alive until user kills it
                .build();
        UserThread.setExecutor(Executors.newSingleThreadExecutor(threadFactory));
    }

    @Override
    protected void launchApplication() {
        bisqDaemon = new BisqDaemon();
        bisqDaemon.setGracefulShutDownHandler(this);
        CommonSetup.setup(bisqDaemon);
        onApplicationLaunched();
    }

    @Override
    protected AppModule getModule() {
        return new CoreModule(bisqEnvironment);
    }

    @Override
    protected void applyInjector() {
        super.applyInjector();
        bisqDaemon.setBisqSetup(injector.getInstance(BisqSetup.class));
        bisqDaemon.setCorruptedDatabaseFilesHandler(injector.getInstance(CorruptedDatabaseFilesHandler.class));
        bisqDaemon.setTradeManager(injector.getInstance(TradeManager.class));
        bisqDaemon.setCoreApi(injector.getInstance(CoreApi.class));
    }

    @Override
    protected void startApplication() {
        bisqDaemon.startApplication();
        onApplicationStarted();
    }
}
