import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'

export const cli = vorpal()

let username
let host = 'localhost'
let port = 8080
let server
let prevCmd

cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  .mode('connect <username> [host] [port]')
  .delimiter(cli.chalk['green']('connected>')) 
  .init(function (args, callback) {
    username = args.username
    if(args.host) host = args.host
    if(args.port) port = args.port
    server = connect({ host, port }, () => {
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      callback()
    })

    server.on('data', (buffer) => {
      this.log(Message.fromJSON(buffer).toString())
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action(function (input, callback) {
    const [ command, ...rest ] = words(input)
    const contents = rest.join(' ')

    if (command === 'disconnect') {
      prevCmd = command
      server.end(new Message({ username, command }).toJSON() + '\n')
    } else if (command === 'echo') {
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (command === 'broadcast'){
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    } else if (input[0] === '@'){
      server.write(new Message({ username, command: 'whisper', contents: contents, wUsername: command  }).toJSON() + '\n')
    } else if(command === 'users'){
      server.write(new Message({ username, command, contents }).toJSON() + '\n')
    }
    else {
      this.log(`Command <${command}> was not recognized`)
    }

    callback()
  })
