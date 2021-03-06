import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'
const readline = require('readline');
export const cli = vorpal()

let host = 'localhost'
let port = 8080
let server
let prevCmd = null
let prevUsername = null
let time = new Date()
let username
let loggedIn = false
let newUser = false



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
      let message = Message.fromJSON(buffer)
      let timestamp = time.toLocaleTimeString()
      if(message.command === 'echo'){
        this.log(cli.chalk['grey'](`${timestamp} <${message.username}> (echo): ${message.contents}`))
      }else if(message.command === 'broadcast' ){
        this.log(cli.chalk['cyan'](`${timestamp} <${message.username}> (all): ${message.contents}`))
      }else if(message.command === 'whisper'){
        this.log(cli.chalk['blue'](`${timestamp} <${message.username}> (whisper): ${message.contents}`))
      }else if(message.command === 'groupchat'){
        this.log(cli.chalk['magenta'](`${timestamp} <${message.username}> (${message.wUsername}): ${message.contents}`))
      }else if(message.command === 'users'){
        this.log(cli.chalk['green'](`${timestamp}: currently connected users:`))
        message.contents.slice(1, message.contents.length - 1).split(', ').forEach( (a) => this.log(cli.chalk['green'](`<${a}>`)) )
      }else if(message.command === 'newuser'){
        this.log(cli.chalk['red'](`${timestamp}: <${message.username}> has connected`))
      }else if(message.command === 'userleft'){
        this.log(cli.chalk['red'](`${timestamp}: <${message.username}> has disconnected`))
      }else if(message.command === 'createdgroup'){
        this.log(cli.chalk['red'](`${timestamp}: group <${message.username}> has been created`))
      }else if(message.command === 'newgroup'){
        this.log(cli.chalk['magenta'](`${timestamp}: ${message.contents}`))
      }else if(message.command === 'joingroup'){
        this.log(cli.chalk['magenta'](`${timestamp}: ${message.contents}`))
      }else if(message.command === 'leavegroup'){
        this.log(cli.chalk['magenta'](`${timestamp}: ${message.contents}`))
      }else if(message.command === 'givepassword'){
        this.log(cli.chalk['red'](`${message.contents}`))
      }else if(message.command === 'givenewpassword'){
        newUser = true
        this.log(cli.chalk['red'](`${message.contents}`))
      }else if(message.command === 'loggedin'){
        loggedIn = true
      }
      else{
        this.log('something went wrong!')
      }
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action(function (input, callback) {
    const [ command, ...rest ] = words(input)
    const contents = rest.join(' ')

    if(!loggedIn){
      if(newUser){
        server.write(new Message({ username, command: 'givenewpassword', contents: input }).toJSON() + '\n')
      }else{
        server.write(new Message({ username, command: 'givepassword', contents: input }).toJSON() + '\n')
      } 
    }else{
      if (command === 'disconnect') {
        prevCmd = null
        loggedIn = false
        newUser = false
        server.end(new Message({ username, command }).toJSON() + '\n')
      } else if (command === 'echo') {
        prevCmd = command
        server.write(new Message({ username, command, contents }).toJSON() + '\n')
      } else if (command === 'broadcast'){
        prevCmd = command
        server.write(new Message({ username, command, contents }).toJSON() + '\n')
      } else if (input[0] === '@'){
        prevCmd = 'whisper'
        prevUsername = command
        server.write(new Message({ username, command: 'whisper', contents: contents, wUsername: command  }).toJSON() + '\n')
      } else if(command === 'users'){
        prevCmd = command
        server.write(new Message({ username, command, contents }).toJSON() + '\n')
      }else if(command === 'newgroup'){
        prevCmd = command
        server.write(new Message({ username, command, contents }).toJSON() + '\n')
      }else if(command === 'joingroup'){
        prevCmd = command
        server.write(new Message({ username, command, contents }).toJSON() + '\n')
      }else if(command === 'leavegroup'){
        prevCmd = command
        server.write(new Message({ username, command, contents }).toJSON() + '\n')
      }else {
        if(prevCmd === 'whisper'){
          server.write(new Message({ username, command: 'whisper', contents: command, wUsername: prevUsername  }).toJSON() + '\n')
        }else if(prevCmd){
          server.write(new Message({ username, command: prevCmd, contents: command }).toJSON() + '\n')
        }else{
          this.log(`Command <${command}> was not recognized`)
        }
      }
    }
    callback()
  })
