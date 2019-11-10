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

package bisq.core;

import bisq.core.btc.Balances;
import bisq.core.offer.Offer;
import bisq.core.offer.OfferBookService;
import bisq.core.presentation.BalancePresentation;
import bisq.core.trade.statistics.TradeStatistics2;
import bisq.core.trade.statistics.TradeStatisticsManager;

import bisq.common.app.Version;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides high level interface to functionality of core Bisq features.
 * E.g. useful for different APIs to access data of different domains of Bisq.
 */
public class CoreApi {
    private final Balances balances;
    private final BalancePresentation balancePresentation;
    private final OfferBookService offerBookService;
    private final TradeStatisticsManager tradeStatisticsManager;

    @Inject
    public CoreApi(Balances balances,
                   BalancePresentation balancePresentation,
                   OfferBookService offerBookService,
                   TradeStatisticsManager tradeStatisticsManager) {
        this.balances = balances;
        this.balancePresentation = balancePresentation;
        this.offerBookService = offerBookService;
        this.tradeStatisticsManager = tradeStatisticsManager;
    }

    public String getVersion() {
        return Version.VERSION;
    }

    public long getAvailableBalance() {
        return balances.getAvailableBalance().get().getValue();
    }

    public String getAvailableBalanceAsString() {
        return balancePresentation.getAvailableBalance().get();
    }

    public List<TradeStatistics2> getTradeStatistics() {
        return new ArrayList<>(tradeStatisticsManager.getObservableTradeStatisticsSet());
    }

    public List<Offer> getOffers() {
        return offerBookService.getOffers();
    }
}
