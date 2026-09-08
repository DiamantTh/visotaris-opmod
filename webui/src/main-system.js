import './styles/app.css'
import App from './pages/system/App.svelte'
import { mount } from 'svelte'

mount(App, { target: document.getElementById('app') })
