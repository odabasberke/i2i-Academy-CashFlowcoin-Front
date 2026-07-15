import { useState } from 'react'
import Navbar from '../components/layout/Navbar'
import MarketTicker from '../components/market/MarketTicker'
import MarketTable from '../components/market/MarketTable'
import WalletList from '../components/portfolio/WalletList'
import TransactionHistory from '../components/portfolio/TransactionHistory'
import AiChatPanel from '../components/ai/AiChatPanel'
import TradeModal from '../components/trading/TradeModal'
import { MarketProvider } from '../context/MarketContext'
import { PortfolioProvider } from '../context/PortfolioContext'

export default function DashboardPage() {
  const [selectedSymbol, setSelectedSymbol] = useState(null)

  return (
    <MarketProvider>
      <PortfolioProvider>
        <div className="min-h-screen">
          <Navbar />
          <MarketTicker />

          <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6">
            <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
              <div className="space-y-6 lg:col-span-2">
                <MarketTable onSelectAsset={setSelectedSymbol} />
                <TransactionHistory />
              </div>

              <div className="space-y-6">
                <WalletList />
                <div className="h-[28rem]">
                  <AiChatPanel />
                </div>
              </div>
            </div>
          </main>

          {selectedSymbol && (
            <TradeModal symbol={selectedSymbol} onClose={() => setSelectedSymbol(null)} />
          )}
        </div>
      </PortfolioProvider>
    </MarketProvider>
  )
}
