/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow
 */

import React, { Component } from 'react'
import { Platform, StyleSheet, Button, Text, TextInput, View } from 'react-native'
import ToastExample from './ToastExample'
import CustomDialog from './CustomDialog'
import SecureTextInput from './SecureTextInput'
import SecureTextInput2 from './SecureTextInput2'

const instructions = Platform.select({
  ios: 'Press Cmd+R to reload,\n' + 'Cmd+D or shake for dev menu',
  android:
    'Double tap R on your keyboard to reload,\n' +
    'Shake or press menu button for dev menu',
});

function showNativeDialog() {
  ToastExample.show('Showing native dialog', ToastExample.SHORT)
  CustomDialog.show()
}

type Props = {
  text: string
};

export default class App extends Component<Props> {
    state = { text: "Initial text" }

    onChangeTextInput(text) {
      this.setState({text})
    }

    render() {
      return (
        <View style={styles.container}>
          <Text style={styles.welcome}>Welcome to React Native!</Text>
          <Text style={styles.instructions}>To get started, edit App.js</Text>
          <Text style={styles.instructions}>{instructions}</Text>
          <Button key="button" onPress={showNativeDialog} title="Show Native Dialog" />
          {/* <TextInput value={this.state.text} onChangeText={(text) => this.onChangeTextInput(text)} /> */}
          <Text key="instructions" style={styles.instructions}>{this.state.text}</Text>
          <SecureTextInput key="secure" registrationID="XYZ" />
          <SecureTextInput2 key="secure" registrationID="XYZ" />
          <SecureTextInput2 key="secure" registrationID="XYZ" />
        </View>
      );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
  },
  instructions: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  },
});
