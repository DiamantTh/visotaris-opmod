import test from 'node:test'
import assert from 'node:assert/strict'
import { currencyDisplayName, fmtMoney } from '../src/lib/utils.js'

test('keeps the visible merchant currency names canonical', () => {
  assert.equal(currencyDisplayName('opshards'), 'OPShards')
  assert.equal(currencyDisplayName('redcoins'), 'Redcoins')
  assert.equal(currencyDisplayName('OPS'), 'Unbekannte Währung')
  assert.equal(currencyDisplayName('RDC'), 'Unbekannte Währung')
})

test('formats market and auction amounts with the existing dollar formatter', () => {
  assert.equal(fmtMoney(600000), '$600.000,00')
  assert.equal(fmtMoney(null), '–')
})
