
## Precondition

Manually ✋ operations : 

1. following dir `scripts` should be copied on `pagopa<ENV>weunodombdst` into shared folder `firmatore`
2. unzip into `pagopa<ENV>weunodombdst` into shared folder `firmatore` the content of `firmatore-<ENV>.zip` and `log` folder under `SID-Flussi-e-bollo`
> retrive zip file [here](https://drive.google.com/drive/u/0/folders/1NCVqUhH_Zpy8B68WWSGRUypokCE6twuH)
3. change all paths in `SID-Flussi-e-bollo/config/predisposizione.cfg` and `SID-Flussi-e-bollo/config/ricevute.cfg` relative to ARG* from `/mnt/SID-Flussi-e-bollo/...` to `/mnt/file-azure/firmatore/SID-Flussi-e-bollo/...`

----

Crons orders  ( init.sh is used )
1. runflussi.sh 
2. renametransfer.sh
3. get-elab-ricevute.sh
