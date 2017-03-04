(function(f){if(typeof exports==="object"&&typeof module!=="undefined"){module.exports=f()}else if(typeof define==="function"&&define.amd){define([],f)}else{var g;if(typeof window!=="undefined"){g=window}else if(typeof global!=="undefined"){g=global}else if(typeof self!=="undefined"){g=self}else{g=this}g.mithrilTransition=f()}})(function(){var define,module,exports;return function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);var f=new Error("Cannot find module '"+o+"'");throw f.code="MODULE_NOT_FOUND",f}var l=n[o]={exports:{}};t[o][0].call(l.exports,function(e){var n=t[o][1][e];return s(n?n:e)},l,l.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s}({1:[function(require,module,exports){"use strict";Object.defineProperty(exports,"__esModule",{value:true});function _defineProperty(obj,key,value){if(key in obj){Object.defineProperty(obj,key,{value:value,enumerable:true,configurable:true,writable:true})}else{obj[key]=value}return obj}var enabled=Symbol("enabled");function addClass(elem,className){if(elem.classList){elem.classList.add(className)}else{elem.className+=" "+className}return elem}function removeClass(elem,className){if(elem.classList){elem.classList.remove(className)}else{elem.className=elem.className.replace(new RegExp("(^|\\b)"+className.split(" ").join("|")+"(\\b|$)","gi")," ")}return elem}function persistHistory(key,history){if(key){sessionStorage.setItem(key,JSON.stringify(history))}}function clearHistory(key){if(key){sessionStorage.removeItem(key)}}function createHistory(){var persistHistoryAs=arguments.length<=0||arguments[0]===undefined?null:arguments[0];var history=[];if(persistHistoryAs){if(sessionStorage.getItem(persistHistoryAs)){history=JSON.parse(sessionStorage.getItem(persistHistoryAs))}}return{persistHistoryAs:persistHistoryAs,history:history,push:function push(key){this.history.push(key);persistHistory(this.persistHistoryAs,this.history)},pop:function pop(){this.history.pop();persistHistory(this.persistHistoryAs,this.history)},clear:function clear(){this.history=[];clearHistory(this.persistHistoryAs)},last:function last(){return this.history[this.history.length-1]?this.history[this.history.length-1]:undefined}}}function loadClasses(classList,lastElem,elem,direction){addClass(elem.parentNode,classList.parent);addClass(lastElem,classList.lastElem);addClass(elem,classList.newElem);addClass(elem,classList.direction.replace("<direction>",direction))}function unloadClasses(classList,barrier,parentNode,elem,direction){var newBarrier=barrier-1;if(newBarrier>0){return newBarrier}removeClass(elem,classList.newElem);removeClass(elem,classList.direction.replace("<direction>",direction));if(elem.parentNode){removeClass(elem.parentNode,classList.parent)}return newBarrier}function config(key,elem,isInit,ctx){var _this=this;if(this.useHistory&&!key){throw new Error("Error in mithril-transition: "+"is required specified a key for the v-node.")}if(!isInit&&this.isEnabled()){(function(){var parentNode=elem.parentNode;var dataState={parent:{height:parentNode.offsetHeight,width:parentNode.offsetWidth},lastElem:{height:_this.last?_this.last.state.height:null,width:_this.last?_this.last.state.width:null},newElem:{height:elem.offsetHeight,width:elem.offsetWidth}};var direction="next";if(_this.useHistory){if(_this.history.last()===key){direction="prev";_this.history.pop()}else if(_this.last){_this.history.push(_this.last.key)}}if(_this.last){(function(){var lastElem=_this.last.elem;var id="mithril-transition-"+Date.now();loadClasses(_this.classList,lastElem,elem,direction);lastElem.setAttribute("data-transition-id",id);parentNode.insertAdjacentHTML("beforeend",lastElem.outerHTML);_this.last.elem=lastElem=parentNode.querySelector("[data-transition-id="+id+"]");var barrier=2;_this.anim(lastElem,elem,direction,function(){lastElem.parentNode.removeChild(lastElem);barrier=unloadClasses(_this.classList,barrier,parentNode,elem,direction)},function(){barrier=unloadClasses(_this.classList,barrier,parentNode,elem,direction)},dataState)})()}var userOnUnload=ctx.onunload;ctx.onunload=function(){elem.style["pointer-events"]="none";_this.last={key:key,elem:elem,state:{height:dataState.newElem.height,width:dataState.newElem.width}};if(userOnUnload){userOnUnload()}}})()}}function transition(){var _that;var _ref=arguments.length<=0||arguments[0]===undefined?{}:arguments[0];var anim=_ref.anim;var _ref$useHistory=_ref.useHistory;var useHistory=_ref$useHistory===undefined?true:_ref$useHistory;var _ref$persistHistoryAs=_ref.persistHistoryAs;var persistHistoryAs=_ref$persistHistoryAs===undefined?null:_ref$persistHistoryAs;var _ref$classList=_ref.classList;var classList=_ref$classList===undefined?{parent:"m-transition-parent",lastElem:"m-transition-last-element",newElem:"m-transition-new-element",direction:"m-transition-<direction>"}:_ref$classList;var _ref$disable=_ref.disable;var _disable=_ref$disable===undefined?function(){}:_ref$disable;if(!anim){throw new Error("Error in mithril-transition: "+"option `anim` is required.")}var that=(_that={useHistory:useHistory,anim:anim,config:config,classList:classList},_defineProperty(_that,enabled,true),_defineProperty(_that,"isEnabled",function isEnabled(){return that[enabled]}),_defineProperty(_that,"enable",function enable(){that[enabled]=true}),_defineProperty(_that,"disable",function disable(){_disable();that[enabled]=false}),_that);if(that.useHistory){that.history=createHistory(persistHistoryAs)}var animate=function animate(elem,isInit,ctx){that.config(this.attrs.key,elem,isInit,ctx)};animate.isEnabled=that.isEnabled;animate.enable=that.enable;animate.disable=that.disable;return animate}exports.default=transition;module.exports=exports["default"]},{}]},{},[1])(1)});
