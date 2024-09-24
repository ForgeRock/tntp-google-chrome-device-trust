# Google Chrome Device Trust

The Google Chrome Device Trust node lets administrators authenticate users against the Verified Access API.

## Compatibility

<table>
  <colgroup>
    <col>
    <col>
  </colgroup>
  <thead>
  <tr>
    <th>Product</th>
    <th>Compatible?</th>
  </tr>
  </thead>
  <tbody>
  <tr>
    <td><p>ForgeRock Identity Cloud</p></td>
    <td><p><span>Yes</span></p></td>
  </tr>
  <tr>
    <td><p>ForgeRock Access Management (self-managed)</p></td>
    <td><p><span>Yes</span></p></td>
  </tr>
  <tr>
    <td><p>ForgeRock Identity Platform (self-managed)</p></td>
    <td><p><span>Yes</span></p></td>
  </tr>
  </tbody>
</table>

## Inputs

No external inputs required.

## Configuration

<table>
  <thead>
  <th>Property</th>
  <th>Usage</th>
  </thead>

  <tr>
    <td>API Key</td>
      <td>Google Cloud API Key.
      </td>
  </tr>
  <tr>
    <td>Credentials Client Email</td>
    <td>Google Admin Credentials Client Email.
    </td>

  </tr>
  <tr>
    <td>Private Key</td>
    <td>Google Admin Credentials Private Key.
    </td>
  </tr>
</table>

## Outputs

None

## Outcomes

`Continue` Successfully verified and redirected the user.

`Error` There was an error during the verification process.

## Troubleshooting

If this node logs an error, review the log messages to find the reason for the error and address the issue appropriately.