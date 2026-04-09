import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap-icons/font/bootstrap-icons.min.css'
import './styles/app.css'
import App from './pages/shard/App.svelte'
import { mount } from 'svelte'

mount(App, { target: document.getElementById('app') })
