package io.treehouses.remote.ui.sshtunnel

import android.app.AlertDialog
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.treehouses.remote.Constants
import io.treehouses.remote.R
import io.treehouses.remote.Tutorials
import io.treehouses.remote.databinding.ActivityTunnelSshFragmentBinding
import io.treehouses.remote.databinding.DialogSshtunnelHostsBinding
import io.treehouses.remote.databinding.DialogSshtunnelKeyBinding
import io.treehouses.remote.databinding.DialogSshtunnelPortsBinding
import io.treehouses.remote.pojo.enum.Status
import io.treehouses.remote.utils.*

class SSHTunnelFragment : BaseSSHTunnelFragment() {

    private var bindingDialogHosts : DialogSshtunnelHostsBinding = DialogSshtunnelHostsBinding.inflate(layoutInflater)
    private var bindingDialogPorts : DialogSshtunnelPortsBinding = DialogSshtunnelPortsBinding.inflate(layoutInflater)
    private var bindingDialogKeys : DialogSshtunnelKeyBinding = DialogSshtunnelKeyBinding.inflate(layoutInflater)

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        bind = ActivityTunnelSshFragmentBinding.inflate(inflater, container, false)
        viewModel.onCreateView()
        loadObservers1()
        loadDialogObservers()
        initializeDialog()
        addListeners1()
        addListeners2()
        return bind.root
    }

    private fun loadDialogObservers() {
        viewModel.tunnelSSHKeyDialogData.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            when (it.status) {
                Status.SUCCESS -> {
                    if (it.data!!.showHandleDifferentKeysDialog) handleDifferentKeys(it.data)
                    if (it.data!!.showHandlePhoneKeySaveDialog)
                        handlePhoneKeySave(it.data)
                    if (it.data!!.showHandlePiKeySaveDialog)
                        handlePiKeySave(it.data.profile, it.data.storedPublicKey, it.data.storedPrivateKey)
                }
                Status.LOADING -> TODO()
                Status.ERROR -> TODO()
                Status.NOTHING -> TODO()
            }
        })
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Tutorials.tunnelSSHTutorials(bind, requireActivity())
        viewModel.setUserVisibleHint()
    }

    fun showDialog(dialog: Dialog) {
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show()
    }

    private fun addListeners1() {
        bind.switchNotification.setOnCheckedChangeListener { _, isChecked -> viewModel.switchButton(isChecked) }
        bind.btnAddPort.setOnClickListener { showDialog(dialogPort) };
        bind.btnAddHosts.setOnClickListener {
            logD("treehouses clicked add host")
            showDialog(dialogHosts) }
        bind.btnKeys.setOnClickListener { showDialog(dialogKeys) };
        bind.notifyNow.setOnClickListener { viewModel.notifyNow(requireContext()) }
        bind.info.setOnClickListener {
            val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.CustomAlertDialogStyle)); builder.setTitle("SSH Help")
            builder.setMessage(R.string.ssh_info);
            val dialog = builder.create();
            dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent); dialog.show();
        }
        bindingDialogPorts.btnAddingPort.setOnClickListener { handleAddPort() }
        bindingDialogHosts.btnAddingHost.setOnClickListener {
            val m1 = bindingDialogHosts.PortNumberInput.text.toString()
            val m2 = bindingDialogHosts.UserNameInput.text.toString() + "@" + bindingDialogHosts.DomainIPInput.text.toString()
            viewModel.addingHostButton(m1, m2)
            dialogHosts.dismiss()
        }
    }

    private fun handleAddPort() {
        if (bindingDialogPorts.ExternalTextInput.text!!.isNotEmpty() && bindingDialogPorts.InternalTextInput.text!!.isNotEmpty()) {
            val parts = bindingDialogPorts.hosts?.selectedItem.toString().split(":")[0]
            viewModel.addingPortButton(bindingDialogPorts.InternalTextInput.text.toString(), bindingDialogPorts.ExternalTextInput.text.toString(), parts)
            dialogPort.dismiss()
        }
    }

    private fun addListeners2() {
        var profile = dialogKeys.findViewById<EditText>(R.id.sshtunnel_profile).text.toString()
        bindingDialogKeys.btnSaveKeys.setOnClickListener { viewModel.keyClickListener(profile); }
        bindingDialogKeys.btnShowKeys.setOnClickListener {
            viewModel.keyClickListener(profile); viewModel.handleShowKeys(profile)
        }
        bindingDialogPorts.addPortCloseButton.setOnClickListener { dialogPort.dismiss() }
        bindingDialogHosts.addHostCloseButton.setOnClickListener { dialogHosts.dismiss() }
        bindingDialogKeys.addKeyCloseButton.setOnClickListener { dialogKeys.dismiss() }
        bind.sshPorts.onItemClickListener = AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
           handleDeletePort(position)
        }
    }

    private fun handleDeletePort(position: Int) {
        if (portsName!!.size > 1 && position == portsName!!.size - 1) {
            DialogUtils.createAlertDialog(context, "Delete All Hosts and Ports?") { viewModel.deleteHostPorts() }
        } else {
            val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.CustomAlertDialogStyle))
            if (portsName!![position].contains("@")) {
                setPortDialog(builder, position, "Delete Host  ")
            } else {
                setPortDialog(builder, position, "Delete Port ")
            }
            builder.setNegativeButton("Cancel", null)
            val dialog = builder.create()
            dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent); dialog.show()
        }
    }


    private fun loadObservers1() {
        viewModel.tunnelSSHData.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            when (it.status) {
                Status.SUCCESS -> {
                    logD("treehouses observer " + it.data?.enableAddHost)
                    bind.notifyNow.isEnabled = it.data!!.enabledNotifyNow
                    bind.switchNotification.isEnabled = it.data.enableSwitchNotification; bind.btnAddHosts.text = it.data.addHostText
                    bind.btnAddPort.text = it.data.addPortText; bind.btnAddPort.isEnabled = it.data.enableAddPort
                    bind.btnAddHosts.isEnabled = it.data.enableAddHost; bind.sshPorts.isEnabled = it.data.enableSSHPort
                    bindingDialogKeys.publicKey.text = it.data.publicKey; bindingDialogKeys.privateKey.text = it.data.privateKey
                    bindingDialogKeys.privateKey.visibility = View.GONE
                    portsName = it.data.portNames; hostsName = it.data.hostNames
                    adapter = TunnelUtils.getPortAdapter(requireContext(), portsName)
                    bind.sshPorts.adapter = adapter
                    adapter2 = ArrayAdapter(requireContext(), R.layout.support_simple_spinner_dropdown_item, hostsName!!)
                    bindingDialogPorts.hosts.adapter = adapter2
                }
                Status.LOADING -> {
                    if (it == null) return@Observer
                    bindingDialogKeys.progressBar.visibility = View.VISIBLE
                }
                Status.ERROR -> TODO()
                Status.NOTHING -> TODO()
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun initializeDialog() {
        dialogPort = Dialog(requireContext()); dialogHosts = Dialog(requireContext()); dialogKeys = Dialog(requireContext())
        dialogPort.setContentView(R.layout.dialog_sshtunnel_ports); dialogHosts.setContentView(R.layout.dialog_sshtunnel_hosts)
        dialogKeys.setContentView(R.layout.dialog_sshtunnel_key)
        addHostSyntaxCheck(bindingDialogHosts.UserNameInput, bindingDialogHosts.TLusername, Constants.userRegex, Constants.hostError)
        addHostSyntaxCheck(bindingDialogHosts.DomainIPInput, bindingDialogHosts.TLdomain, Constants.domainRegex + "|" + Constants.ipRegex, Constants.domainIPError)
        addHostSyntaxCheck(bindingDialogHosts.PortNumberInput, bindingDialogHosts.TLportname, Constants.portRegex, Constants.portError)
        addPortSyntaxCheck(bindingDialogPorts.ExternalTextInput, bindingDialogPorts.TLexternal)
        addPortSyntaxCheck(bindingDialogPorts.InternalTextInput, bindingDialogPorts.TLinternal)
        viewModel.initializeArrays()
        val window = dialogPort.window;
        val windowHost = dialogHosts.window
        window!!.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        windowHost!!.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        windowHost.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

//
//    override fun setUserVisibleHint(visible: Boolean) {
//        if (visible) {
//
//        }
//    }

    fun checkAddingHostButtonEnable() {
        if (bindingDialogHosts.UserNameInput.editableText.isNotEmpty() && bindingDialogHosts.DomainIPInput.editableText.isNotEmpty()
                && bindingDialogHosts.PortNumberInput.editableText.isNotEmpty())
            if (!bindingDialogHosts.TLusername.isErrorEnabled && !bindingDialogHosts.TLdomain.isErrorEnabled && !bindingDialogHosts.TLportname.isErrorEnabled)
                bindingDialogHosts.btnAddingHost.isEnabled = true

    }

    fun checkAddingPortButtonEnable() {
        if (bindingDialogPorts.ExternalTextInput.editableText.isNotEmpty() && bindingDialogPorts.InternalTextInput.editableText.isNotEmpty())
            if (!bindingDialogPorts.TLexternal.isErrorEnabled && !bindingDialogPorts.TLinternal.isErrorEnabled)
                bindingDialogPorts.btnAddingPort.isEnabled = true
    }

    /*
       adds a syntax check to textInputEditText. If input in textInputEditText does not match regex, outputs error message in textInputLayout
       and disables addingHostButton
         */
    fun addHostSyntaxCheck(textInputEditText: TextInputEditText, textInputLayout: TextInputLayout, regex: String, error: String) {
        textInputEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                textInputLayout.isErrorEnabled = true
                if (s!!.isEmpty()) {
                    bindingDialogHosts.btnAddingHost.isEnabled = false
                } else {
                    if (!s!!.toString().matches(regex.toRegex())) {
                        bindingDialogHosts.btnAddingHost.isEnabled = false
                        textInputLayout.error = error
                    } else {
                        textInputLayout.isErrorEnabled = false
                        checkAddingHostButtonEnable()
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    fun addPortSyntaxCheck(textInputEditText: TextInputEditText, textInputLayout: TextInputLayout) {
        textInputEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                textInputLayout.isErrorEnabled = true
                if (s!!.isEmpty()) {
                    bindingDialogPorts.btnAddingPort.isEnabled = false
                } else {
                    if (!s!!.toString().matches(Constants.portRegex.toRegex())) {
                        bindingDialogPorts.btnAddingPort.isEnabled = false
                        textInputLayout.error = Constants.portError
                    } else if (textInputEditText == bindingDialogPorts.ExternalTextInput && viewModel.searchArray(portsName, s!!.toString())) {
                        bindingDialogPorts.btnAddingPort.isEnabled = false
                        bindingDialogPorts.TLexternal.error = "Port number already exists"
                    } else {
                        textInputLayout.isErrorEnabled = false
                        checkAddingPortButtonEnable()
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
}

