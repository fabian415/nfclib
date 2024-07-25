echo build SMC
set SMC=../../../../../../../gradle/Smc.jar
java -jar %SMC% -java NFCState.sm
java -jar %SMC% -graph -glevel 2 NFCState.sm
dot -T png -o NFCState_sm.png NFCState_sm.dot

