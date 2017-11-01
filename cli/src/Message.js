export class Message {
  static fromJSON (buffer) {
    return new Message(JSON.parse(buffer.toString()))
  }

  constructor ({ username, command, contents, wUsername = null }) {
    this.username = username
    this.command = command
    this.contents = contents
    this.wUsername = wUsername
  }

  toJSON () {
    return JSON.stringify({
      username: this.username,
      command: this.command,
      contents: this.contents,
      wUsername: this.wUsername
    })
  }

  toString () {
    return this.contents
  }
}
