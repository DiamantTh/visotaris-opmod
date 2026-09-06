import './styles/app.css'
import App from './pages/shard/App.svelte'
import { mount } from 'svelte'

mount(App, { target: document.getElementById('app'), props: { currency: 'redcoins' } })
